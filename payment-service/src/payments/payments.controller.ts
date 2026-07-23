import { Controller, Post, Get, Body, Param, HttpCode } from '@nestjs/common';
import { PaymentsService } from './payments.service';
import { CreatePaymentDto } from './dto/create-payment.dto';

@Controller('payments')
export class PaymentsController {
  constructor(private readonly paymentsService: PaymentsService) {}

  @Post()
  @HttpCode(201)
  create(@Body() dto: CreatePaymentDto) {
    return this.paymentsService.create(dto);
  }

  @Get('by-order/:orderId/status')
  statusByOrder(@Param('orderId') orderId: string) {
    return this.paymentsService.statusByOrder(orderId);
  }
}
