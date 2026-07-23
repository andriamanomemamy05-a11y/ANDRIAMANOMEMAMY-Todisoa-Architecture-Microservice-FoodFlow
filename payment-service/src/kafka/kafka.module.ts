import { Module } from '@nestjs/common';
import { PaymentsModule } from '../payments/payments.module';
import { KafkaService } from './kafka.service';

/**
 * Isole tout le Kafka (producer + consumer) dans un module dédié.
 * Importe PaymentsModule pour réutiliser PaymentsService (la logique métier),
 * sans dépendance circulaire : PaymentsModule ne connaît pas Kafka.
 */
@Module({
  imports: [PaymentsModule],
  providers: [KafkaService],
})
export class KafkaModule {}
